fn countdown(n: int) -> int {
    var result: int = 0
    while(n > 0) {
        result = result + n
        n = n - 1
    }
    return result
}

fn main() -> int {
    return countdown(5)
}