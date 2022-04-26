package com.example.back.sql;

import lombok.Data;

@Data
public class Column {
    String expr;
    String resultColumn;
//    String alias;  没必要
    String sourceDbName;
    String sourceTableName;
    Type type;
}
