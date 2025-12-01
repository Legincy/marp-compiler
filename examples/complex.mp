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
    print("hello world")
    print(x)
    var y: int = calculate(2, 3, 4)
    print(x)
    var z: int = nested(10, 2)
    print(x)
    return x + y + z
}