// built in types: uint8, uint16, uint32, uint64, int8, int16, int32, int64, void
// keywords: structure, function, return, internal, typedef, (...types)

structure bruh {
    abc: uint8;
    def: void(uint8);
}

object pog {
    internal field data: uint64;
    pog(uint64 param) {
        data = param;
    }
    function:void test() {
        write(1, "Ran method from pog\n", 20);
    }
}

object more extends pog {
    function:void test() {
        write(1, "Ran method from more\n", 21);
    }
}

typedef string uint64;

function:int64 write(uint64 fd, string ptr, uint64 len) {
    return syscall(1, fd, ptr, len);
}

function:void main() {
    write(1, "Hello world\n", 12);
    pog var1 = private new pog(123);
    var1.test();
    pog var2 = private new more(123);
    var2.test();
}
