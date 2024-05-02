import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def domain = "ddovguchev.atlassian.net"
def username = "ddovguchev@gmail.com" // username и api_token опциональны, данные поддтягиваются с vault
def api_token = <TOKEN>
def pageId = "2392065"

class ConfluenceClient {
  private String domain
  private String username
  private String apiToken
  private String baseApiUrl
  private String basicAuth

  ConfluenceClient(String domain, String username, String apiToken) {
    this.domain = domain
    this.username = username
    this.apiToken = apiToken
    this.baseApiUrl = "https://${domain}/wiki/rest/api/content"
    String userpass = username + ":" + apiToken
    this.basicAuth = "Basic " + userpass.bytes.encodeBase64().toString()
  }

  String getPageContent(String pageId) {
    def connection = new URL("https://${domain}/wiki/rest/api/content/${pageId}?expand=body.storage").openConnection()
    connection.setRequestMethod('GET')
    connection.setRequestProperty('Accept', 'application/json')
    connection.setDoOutput(true)
    connection.setRequestProperty('Authorization', basicAuth)

    if (connection.responseCode == 200) {
      def parser = new JsonSlurper()
      def response = parser.parse(connection.inputStream)
      return "Содержимое страницы: $response.body.storage.value"
    } else {
      return "Error: ${connection.responseCode}"
    }
  }

  void createPage(String title, String content) {
    def connection = new URL(baseApiUrl).openConnection()
    try {
        connection.setRequestMethod("POST")
        connection.setRequestProperty('Accept', 'application/json')
        connection.setRequestProperty('Content-Type', 'application/json')
        connection.setDoOutput(true)
        connection.setRequestProperty('Authorization', basicAuth)

        def jsonBody = [
            type: "page",
            title: title,
            space: [key: "1736711"],
            body: [
                storage: [
                    value: content,
                    representation: "storage"
                ]
            ],
            status: "current"
        ]

        def writer = new OutputStreamWriter(connection.outputStream)
        writer.write(JsonOutput.toJson(jsonBody))
        writer.flush()
        writer.close()

        if (connection.responseCode == 200) {
            def parser = new JsonSlurper()
            def response = parser.parse(connection.inputStream)
            println "Страница была создана с именем: ${response.title} и id: ${response.id}"
        } else {
          println "Ошибка при создании страницы: ${connection.responseCode}"
        }
    } catch (Exception e) {
      println "Произошла ошибка: ${e.message}"
    } finally {
      if (connection != null) {
        connection.disconnect()
      }
    }
  }


  void addPageContent(String pageId, String newContent) {
    def pageResponse = getPageContent(pageId)
    if (pageResponse.startsWith("Error:")) {
      println pageResponse
      return
    }

    def connection = new URL("${baseApiUrl}/${pageId}?expand=version").openConnection()
    connection.setRequestMethod('GET')
    connection.setRequestProperty('Accept', 'application/json')
    connection.setDoOutput(true)
    connection.setRequestProperty('Authorization', basicAuth)

    def currentVersion
    if (connection.responseCode == 200) {
      def parser = new JsonSlurper()
      def response = parser.parse(connection.inputStream)
      currentVersion = response.version.number
    } else {
      println "Ошибка при получении версии страницы: ${connection.responseCode}"
      return
    }
    connection = new URL("${baseApiUrl}/${pageId}").openConnection()
    try {
      connection.setRequestMethod('PUT')
      connection.setRequestProperty('Accept', 'application/json')
      connection.setRequestProperty('Content-Type', 'application/json')
      connection.setDoOutput(true)
      connection.setRequestProperty('Authorization', basicAuth)

      def updatedContent = pageResponse + newContent


      def jsonBody = [
          id: pageId,
          type: "page",
          title: "TEST",
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
      writer.write(JsonOutput.toJson(jsonBody))
      writer.flush()
      writer.close()

      if (connection.responseCode == 200) {
        println "Содержимое страницы успешно обновлено."
      } else {
        println "Ошибка при обновлении содержимого страницы: ${connection.responseCode}"
      }
    } catch (Exception e) {
      println "Произошла ошибка: ${e.message}"
    } finally {
      connection.disconnect()
    }
  }
}

def client = new ConfluenceClient(domain, username, api_token)
//client.createPage('test title 3', 'create by script') // #TODO add realization
println client.getPageContent(pageId)
client.addPageContent(pageId, '<p>new line</p>')
//println client.deletePageById(pageId) #TODO add realization

// #TODO create private method "generate table"