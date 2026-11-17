package com.ganzithon.station.user.controller;

import com.ganzithon.station.global.response.ApiResponse;
import com.ganzithon.station.global.security.CurrentUser;
import com.ganzithon.station.global.security.Requester;
import com.ganzithon.station.user.dto.request.UserLoginRequest;
import com.ganzithon.station.user.dto.request.UserPublicSettingUpdateRequest;
import com.ganzithon.station.user.dto.request.UserSignupRequest;
import com.ganzithon.station.user.dto.response.UserAuthResponse;
import com.ganzithon.station.user.dto.response.UserMeResponse;
import com.ganzithon.station.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "계정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "가입",
            description = """
                    닉네임 + PIN 4자리로 가입하고 계정 토큰을 받습니다. 토큰 없이 호출할 수 있습니다.
                    - 닉네임은 한글·영문·숫자 1~12자, PIN은 숫자 4자리입니다. pin과 pinConfirm이 다르면 400 COMMON_INVALID_INPUT(pinConfirm).
                    - 닉네임이 이미 있으면 409 USER_NICKNAME_DUPLICATED""")
    @PostMapping
    public ResponseEntity<ApiResponse<UserAuthResponse>> signup(@Valid @RequestBody UserSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userService.signup(request)));
    }

    @Operation(summary = "로그인",
            description = """
                    닉네임 + PIN으로 로그인하고 계정 토큰을 받습니다. 토큰 없이 호출할 수 있습니다.
                    - 닉네임이 없거나 PIN이 틀리면 둘 다 401 USER_LOGIN_FAILED
                    - 연속 5회 실패하면 5분간 429 USER_LOGIN_LOCKED (잠금 중에는 PIN이 맞아도 거부)""")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserAuthResponse>> login(@Valid @RequestBody UserLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.login(request)));
    }

    @Operation(summary = "내 정보",
            description = "내 닉네임과 담요 대기실 공개 여부를 봅니다. 계정 토큰이 필요합니다.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> getMe(@CurrentUser Requester requester) {
        return ResponseEntity.ok(ApiResponse.success(userService.getMe(requester.requireUserId())));
    }

    @Operation(summary = "공개 설정 변경",
            description = "담요 대기실 공개 여부를 바꿉니다. 계정 토큰이 필요합니다.")
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponse>> updatePublicSetting(
            @CurrentUser Requester requester,
            @Valid @RequestBody UserPublicSettingUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                userService.updatePublicSetting(requester.requireUserId(), request)));
    }
}
