declaracionPrevia# char letrica
char1 = '5'
letrica = char1
declaracion# int _arr_[20]
declaracionPrevia# int _i_
int2 = 5
_i_ = int2
declaracionPrevia# int arr[3]
int3 = 22
int4 = 10
int5 = 11
arr[0] = int3
arr[1] = int4
arr[2] = int5
declaracionPrevia# float pi
float6 = 3.14
pi = float6
declaracionPrevia# float notas[3]
float7 = 9.5
float8 = 8.0
float9 = 7.25
notas[0] = float7
notas[1] = float8
notas[2] = float9
declaracionPrevia# bool activo
bool10 = true
activo = bool10
declaracionPrevia# string saludo
string11 = "hola"
saludo = string11
goto void$principal

int$f:
int12 = 1
return int12
int$f$final

int$suma:
param_int a
param_int b
declaracionPrevia# int r
int13 = a
int14 = b
int15 = int13 + int14
int16 = 2
int17 = int15 * int16
r = int17
int18 = r
int19 = a
int20 = b
int21 = int19 // int20
int22 = int18 - int21
r = int22
int23 = r
int24 = 3
int25 = int23 mod int24
r = int25
int26 = r
int27 = 2
int28 = 3
int29 = int27 pot int28
int30 = int26 + int29
r = int30
int31 = r
return int31
int$suma$final

float$promedio:
param_float x
param_float y
declaracionPrevia# float total
float32 = x
float33 = y
float34 = float32 + float33
total = float34
float35 = total
float36 = 2.0
float37 = float35 / float36
return float37
float$promedio$final

char$inicial:
param_char c
param_bool mayus
bool38 = mayus
if bool38 goto inicial$if$1$bloque1
goto inicial$if$1$siguiente1

inicial$if$1$bloque1:
char39 = 'A'
return char39

inicial$if$1$siguiente1:

inicial$if$1$final:
char40 = c
return char40
char$inicial$final

bool$distintos:
param_int a
param_int b
int41 = a
int42 = b
bool43 = int41 != int42
return bool43
bool$distintos$final

bool$_funcionInt_:
declaracionPrevia# bool result
bool44 = true
int45 = 12
int46 = 34
int47 = 35
int48 = int46 + int47
bool49 = int45 >= int48
bool50 = not bool49
bool51 = bool44 and bool50
result = bool51

_funcionInt_$while$2$condicion:
bool52 = true
if bool52 goto _funcionInt_$while$2$bloque
goto _funcionInt_$while$2$final

_funcionInt_$while$2$bloque:
int53 = _i_
write int53
goto _funcionInt_$while$2$final
goto _funcionInt_$while$2$condicion

_funcionInt_$while$2$final:
bool54 = true
return bool54
bool$_funcionInt_$final

void$principal:
declaracionPrevia# int var
int55 = 10
var = int55
int56 = 22
int57 = 45
bool58 = int56 < int57
int59 = var
int60 = 5
int61 = 6
int62 = int60 // int61
bool63 = int59 > int62
bool64 = bool58 and bool63
if bool64 goto principal$if$3$bloque1
goto principal$if$3$siguiente1

principal$if$3$bloque1:
declaracion# int y
declaracionPrevia# char ch33
char65 = 'a'
ch33 = char65
goto principal$if$3$final

principal$if$3$siguiente1:
bool66 = true
bool67 = false
bool68 = bool66 and bool67
bool69 = true
bool70 = bool68 or bool69
if bool70 goto principal$if$3$bloque2
goto principal$if$3$siguiente2

principal$if$3$bloque2:
declaracion# int y
goto principal$if$3$final

principal$if$3$siguiente2:
declaracion# int y
declaracionPrevia# string str2
string71 = "sdff"
str2 = string71

principal$if$3$final:
declaracion# int x
declaracionPrevia# int edades[3]
int72 = 22
int73 = 10
int74 = 11
edades[0] = int72
edades[1] = int73
edades[2] = int74

principal$for$4$inicializacion:
int75 = 0
x = int75

principal$for$4$condicion:
goto principal$for$4$bloque

principal$for$4$incremento:
int76 = x
int77 = int76 + 1
x = int77
goto principal$for$4$condicion

principal$for$4$bloque:
string78 = "Hola mundo"
write string78
int79 = x
int80 = 10
bool81 = int79 > int80
if bool81 goto principal$if$5$bloque1
goto principal$if$5$siguiente1

principal$if$5$bloque1:
int82 = edades[2]
int83 = 1
int84 = int82 + int83
edades[2] = int84
goto principal$for$4$final

principal$if$5$siguiente1:

principal$if$5$final:
goto principal$for$4$incremento

principal$for$4$final:
declaracionPrevia# int uno
int85 = call int$f, 0
uno = int85
declaracionPrevia# int s
int86 = 3
int87 = 4
param int86
param int87
int88 = call int$suma, 2
s = int88
int89 = _i_
int90 = 2
param int89
param int90
int91 = call int$suma, 2
declaracionPrevia# float prom
float92 = 7.5
float93 = pi
param float92
param float93
float94 = call float$promedio, 2
prom = float94
declaracionPrevia# char c
char95 = letrica
bool96 = activo
param char95
param bool96
char97 = call char$inicial, 2
c = char97
declaracionPrevia# bool dif
int98 = s
int99 = 7
param int98
param int99
bool100 = call bool$distintos, 2
dif = bool100
declaracionPrevia# bool resultadoBool
bool101 = call bool$_funcionInt_, 0
resultadoBool = bool101
read var
read c
read _arr_[0]
int102 = s
write int102
float103 = prom
write float103
string104 = saludo
write string104
int105 = -5
write int105
declaracionPrevia# int contador
int106 = 3
contador = int106

principal$while$6$condicion:
int107 = contador
int108 = 0
bool109 = int107 > int108
if bool109 goto principal$while$6$bloque
goto principal$while$6$final

principal$while$6$bloque:
int110 = contador
int111 = 3
bool112 = int110 == int111
if bool112 goto principal$if$7$bloque1
goto principal$if$7$siguiente1

principal$if$7$bloque1:
string113 = "tres"
write string113
goto principal$if$7$final

principal$if$7$siguiente1:
int114 = contador
int115 = 2
bool116 = int114 == int115
if bool116 goto principal$if$7$bloque2
goto principal$if$7$siguiente2

principal$if$7$bloque2:
string117 = "dos"
write string117
goto principal$if$7$final

principal$if$7$siguiente2:
int118 = contador
int119 = 1
bool120 = int118 <= int119
if bool120 goto principal$if$7$bloque3
goto principal$if$7$siguiente3

principal$if$7$bloque3:
string121 = "uno"
write string121

principal$if$7$siguiente3:

principal$if$7$final:
int122 = contador
int123 = 1
int124 = int122 - int123
contador = int124
goto principal$while$6$condicion

principal$while$6$final:
char125 = c
char126 = 'A'
bool127 = char125 == char126
bool128 = not bool127
if bool128 goto principal$if$8$bloque1
goto principal$if$8$siguiente1

principal$if$8$bloque1:
string129 = "no es A"
write string129

principal$if$8$siguiente1:

principal$if$8$final:

principal$for$9$inicializacion:
declaracionPrevia# int i
int130 = 0
i = int130

principal$for$9$condicion:
int131 = i
int132 = 3
bool133 = int131 < int132
if bool133 goto principal$for$9$bloque
goto principal$for$9$final

principal$for$9$incremento:
int134 = i
int135 = int134 + 1
i = int135
goto principal$for$9$condicion

principal$for$9$bloque:
int136 = arr[i]
int137 = 2
int138 = int136 * int137
arr[i] = int138
float139 = notas[i]
float140 = 2.0
float141 = float139 / float140
notas[i] = float141
int142 = i
int143 = 1
int144 = int142 + int143
int145 = arr[i]
_arr_[int144] = int145
goto principal$for$9$incremento

principal$for$9$final:
declaracion# int j

principal$for$10$inicializacion:
int146 = 10
j = int146

principal$for$10$condicion:
int147 = j
int148 = 0
bool149 = int147 >= int148
if bool149 goto principal$for$10$bloque
goto principal$for$10$final

principal$for$10$incremento:
int150 = j
int151 = int150 - 1
j = int151
goto principal$for$10$condicion

principal$for$10$bloque:
int152 = j
int153 = 5
bool154 = int152 < int153
bool155 = activo
bool156 = bool154 or bool155
if bool156 goto principal$if$11$bloque1
goto principal$if$11$siguiente1

principal$if$11$bloque1:

principal$while$12$condicion:
int157 = j
int158 = 2
bool159 = int157 > int158
if bool159 goto principal$while$12$bloque
goto principal$while$12$final

principal$while$12$bloque:
int160 = j
int161 = 1
int162 = int160 - int161
j = int162
goto principal$while$12$final
goto principal$while$12$condicion

principal$while$12$final:
goto principal$for$10$final

principal$if$11$siguiente1:

principal$if$11$final:
goto principal$for$10$incremento

principal$for$10$final:
return
void$principal$final
