#!/usr/bin/env bash
# Headless design workbench for Fayber Config.
#
# Boots the dev client on a private Xvfb display, waits for the preview hook to open the demo
# config screen, grabs a single frame with ffmpeg, then tears everything down. Lets the GUI be
# iterated on from a machine with no display.
#
# Usage: tools/preview.sh [output.png] [gui_scale] [wait_seconds] [scroll_set]
# scroll_set: settle the demo list at this scroll offset (in GUI px) before the capture,
# e.g. 19.5 for a fractional offset to inspect sub-pixel rendering.
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${1:-/tmp/fayberconfig-preview.png}"
GUI_SCALE="${2:-3}"
WAIT="${3:-90}"
SCROLL_SET="${4:-}"
DISPLAY_NUM="${FC_DISPLAY:-:97}"
W=1280
H=720
JDK="${FC_JDK:-/home/fayber/.jdks/jdk-25.0.4+7}"

cleanup() {
	[[ -n "${GRADLE_PID:-}" ]] && kill -9 "$GRADLE_PID" 2>/dev/null
	pkill -f "fayberconfig.preview=true" 2>/dev/null
	[[ -n "${XVFB_PID:-}" ]] && kill -9 "$XVFB_PID" 2>/dev/null
	return 0
}
trap cleanup EXIT

# Force the GUI scale the screenshot should be judged at (the whole point is scale independence,
# so this is the knob to sweep).
mkdir -p "$HERE/run"
if [[ -f "$HERE/run/options.txt" ]]; then
	sed -i "s/^guiScale:.*/guiScale:${GUI_SCALE}/" "$HERE/run/options.txt"
	grep -q '^guiScale:' "$HERE/run/options.txt" || echo "guiScale:${GUI_SCALE}" >>"$HERE/run/options.txt"
else
	printf 'guiScale:%s\n' "$GUI_SCALE" >"$HERE/run/options.txt"
fi

Xvfb "$DISPLAY_NUM" -screen 0 "${W}x${H}x24" >/dev/null 2>&1 &
XVFB_PID=$!
sleep 2

LOG=/tmp/fayberconfig-preview.log
: >"$LOG"
(
	cd "$HERE" || exit 1
	DISPLAY="$DISPLAY_NUM" ./gradlew runClient -PfcPreview=true \
		${SCROLL_SET:+-PfcPreviewScroll=$SCROLL_SET} \
		-Dorg.gradle.java.home="$JDK" >>"$LOG" 2>&1
) &
GRADLE_PID=$!

# Wait for the hook to report that the demo screen is up.
for ((i = 0; i < WAIT; i++)); do
	if grep -q "PREVIEW: opening demo screen" "$LOG" 2>/dev/null; then
		# Software rendering under Xvfb can run ticks slower than 20/s and the optional
		# scroll offset is applied ~1s (of ticks) after the screen opens, so wait generously.
		sleep 6
		DISPLAY="$DISPLAY_NUM" ffmpeg -v error -f x11grab -video_size "${W}x${H}" \
			-i "$DISPLAY_NUM" -frames:v 1 -y "$OUT" </dev/null
		echo "captured: $OUT"
		exit 0
	fi
	sleep 1
done

echo "TIMEOUT: preview screen never opened, tail of $LOG:"
tail -30 "$LOG"
exit 1
