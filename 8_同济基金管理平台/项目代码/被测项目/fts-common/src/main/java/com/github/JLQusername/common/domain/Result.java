package com.github.JLQusername.common.domain;

//统一响应结果
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class Result {
    private Integer code;//业务状态码  0-成功  1-失败
    private String msg;//提示信息
    private Object data;//响应数据

    public static Result success(Object data) {
        return new Result(0, "操作成功", data);
    }

    public static Result success() {
        return new Result(0, "操作成功", null);
    }

    public static Result error(String message) {
        return new Result(1, message, null);
    }
}
