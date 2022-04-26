const {defineConfig} = require('@vue/cli-service')
module.exports = defineConfig({
    transpileDependencies: true,
    devServer: {
        proxy: {
            '/api': { // 匹配路径
                target: 'http://localhost:8319/api',
                changeOrigin: true,
                pathRewrite: {
                    '^/api': '',
                }
            }
        }
    }
})
