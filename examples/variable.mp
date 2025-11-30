var counter: int = 0
var multiplier: int = 10

fn increment() -> int {
    counter = counter + 1
    return counter
}

fn main() -> int {
    var result: int = increment()
    return result * multiplier
}