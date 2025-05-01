enum class AgentUseCode(val alias: Int) {
    USE(1),
    NOT(0),
}

enum class ResCode(val alias: Int) {
    SUC(0),
    AUTH_ERR(401),
    PARAM_ERR(400),
    SERVE_ERR(500),
}
