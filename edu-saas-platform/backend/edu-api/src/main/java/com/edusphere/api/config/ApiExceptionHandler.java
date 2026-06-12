package com.edusphere.api.config;

import com.edusphere.common.api.ApiResult;
import com.edusphere.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /**
     * 是否在500响应中携带异常详情（开发环境开启，生产环境应关闭避免泄露内部信息）
     */
    private final boolean includeErrorDetail;

    public ApiExceptionHandler(@Value("${edu.api.include-error-detail:true}") boolean includeErrorDetail) {
        this.includeErrorDetail = includeErrorDetail;
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResult<Void>> handleBizException(BizException exception, HttpServletRequest request) {
        return ResponseEntity.status(resolveHttpStatus(exception.getCode()))
                .body(ApiResult.fail(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleBadRequest(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiResult.fail(400, "请求体格式错误"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null
                ? "请求参数校验失败"
                : "参数 " + fieldError.getField() + " " + fieldError.getDefaultMessage();
        return ResponseEntity.badRequest().body(ApiResult.fail(400, message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResult<Void>> handleMissingParameter(MissingServletRequestParameterException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiResult.fail(400, "缺少必填参数：" + exception.getParameterName()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResult<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(ApiResult.fail(400, "参数 " + exception.getName() + " 格式不正确"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleNotFound(NoResourceFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResult.fail(404, "接口不存在：" + request.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResult<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResult.fail(405, "请求方法 " + exception.getMethod() + " 不支持"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception exception, HttpServletRequest request) {
        log.error("Unhandled API exception path={}", request.getRequestURI(), exception);
        String message = includeErrorDetail
                ? "服务器内部错误：" + exception.getClass().getSimpleName()
                        + (exception.getMessage() == null ? "" : " - " + firstLine(exception.getMessage()))
                : "系统繁忙，请稍后再试";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.fail(500, message));
    }

    private static String firstLine(String message) {
        int lineBreak = message.indexOf('\n');
        String line = lineBreak > 0 ? message.substring(0, lineBreak) : message;
        return line.length() > 200 ? line.substring(0, 200) + "…" : line;
    }

    private HttpStatus resolveHttpStatus(int code) {
        return HttpStatus.resolve(code) == null ? HttpStatus.BAD_REQUEST : HttpStatus.valueOf(code);
    }
}
