fn max(a: int, b: int) -> int {
    if (a > b) {
        return a
    } elseif (a == b) {
        return 0
    } else {
        return b
    }
}

fn main() -> int {
    return max(10, 5)
}