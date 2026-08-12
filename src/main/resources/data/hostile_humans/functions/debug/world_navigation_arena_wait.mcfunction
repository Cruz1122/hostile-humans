# Compatibility entry point for older sessions. New arena runs schedule the
# start directly, avoiding an unbounded wait when an item pickup is missed.
function hostile_humans:debug/world_navigation_arena_start
