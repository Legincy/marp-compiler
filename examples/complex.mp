fn calculate(a: int, b: int, c: int) -> int {
    return a * b + c
}

fn formula(x: int, y: int, z: int) -> int {
    return x + y * z
}

fn multiply(a: int, b: int) -> int {
    return a * b
}

fn divide(a: int, b: int) -> int {
    return a / b
}

fn nested(a: int, b: int) -> int {
    return multiply(a, b) + divide(a, b)
}

fn main() -> int {
    var x: int = formula(2, 3, 4)
    var y: int = calculate(2, 3, 4)
    var z: int = nested(10, 2)
    return x + y + z
}