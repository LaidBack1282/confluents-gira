import groovy.json.JsonSlurper

def domain = "ddovguchev.atlassian.net"
def username = "ddovguchev@gmail.com"
def api_token = "ATATT3xFfGF0xSqBaS471T02dJqX-YhP_28hMZL-45SMiUWTCJgwfxrCR5gGM1NIZr47VvTDNxQKx_mhSor0nkgE9UDQOXNf3yx4de-Emd-KTnIlVQxoA_h4znLovB14ckWYmmf0SEoRNslBq4dWVps8KCysjFSADPMBhwYxvmUi5qvyfiBo_u8=83110801"

class ConfluenceClient {
    private String domain
    private String username
    private String apiToken

    ConfluenceClient(String domain, String username, String apiToken) {
        this.domain = domain
        this.username = username
        this.apiToken = apiToken
    }

    String getPageContent(String pageId) {
        def connection = new URL("https://${domain}/wiki/rest/api/content/${pageId}?expand=body.storage").openConnection()
        connection.setRequestMethod('GET')
        connection.setRequestProperty('Accept', 'application/json')
        connection.setDoOutput(true)
        String userpass = username + ":" + apiToken
        String basicAuth = "Basic " + userpass.bytes.encodeBase64().toString()
        connection.setRequestProperty('Authorization', basicAuth)

        if (connection.responseCode == 200) {
            def parser = new JsonSlurper()
            def response = parser.parse(connection.inputStream)
            return response.body.storage.value
        } else {
            return "Error: ${connection.responseCode}"
        }
    }

    void addPageContent(String pageId, String newContent) {
        def pageResponse = getPageContent(pageId)
        if (pageResponse.startsWith("Error:")) {
            println pageResponse // Вывод ошибки, если не удалось получить содержимое страницы
            return
        }

        def parser = new JsonSlurper()
        def pageJson = parser.parseText(pageResponse)
        int currentVersion = pageJson.version.number

        def connection = new URL("https://${domain}/wiki/rest/api/content/${pageId}").openConnection()
        try {
            connection.setRequestMethod('PUT')
            connection.setRequestProperty('Accept', 'application/json')
            connection.setRequestProperty('Content-Type', 'application/json')
            connection.setDoOutput(true)
            String userpass = username + ":" + apiToken
            String basicAuth = "Basic " + userpass.bytes.encodeBase64().toString()
            connection.setRequestProperty('Authorization', basicAuth)

            def updatedContent = pageJson.body.storage.value + newContent // Добавление нового содержимого

            def jsonBody = [
                id: pageId,
                type: "page",
                title: pageJson.title,
                body: [
                    storage: [
                        value: updatedContent,
                        representation: "storage"
                    ]
                ],
                version: [
                    number: currentVersion + 1,
                    message: "Обновлено через скрипт"
                ]
            ]

            def writer = new OutputStreamWriter(connection.outputStream)
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            if (connection.responseCode == 200) {
                println "Содержимое страницы успешно обновлено."
            } else {
                println "Ошибка при обновлении содержимого страницы: ${connection.responseCode}"
            }
        } finally {
            connection.disconnect() // Закрытие соединения
        }
    }

}

def client = new ConfluenceClient(domain, username, api_token)
//println client.getPageContent('1835012')
println client.addPageContent('1835012', '<p>new line</p>')