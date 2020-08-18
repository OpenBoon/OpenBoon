if (!Cypress.env('USERNAME') || !Cypress.env('PASSWORD')) {
  throw new Error('Please provide a username and password.')
}

Cypress.on('uncaught:exception', () => {
  return false
})

/**
 * getCookies()
 */
function getCookies() {
  return Object.fromEntries(
    document.cookie.split(/; */).map((c) => {
      const [key, ...v] = c.split('=')
      return [key, decodeURIComponent(v.join('='))]
    }),
  )
}

/**
 * Extends the `cy.request()` function to have the headers needed to use the
 * Wallet REST API. For convenience it allows you to set an array of acceptable
 * status codes for the response as well.
 */
Cypress.Commands.add(
  'fetch',
  ({ url, method = 'GET', body, okStatusCodes = [200, 201, 207], ...rest }) => {
    cy.request({
      url,
      method,
      body,
      failOnStatusCode: false,
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': getCookies().csrftoken,
        Referer: Cypress.config('baseUrl'),
      },
      ...rest,
    }).then(({ status }) => {
      if (!okStatusCodes.includes(status)) {
        throw new Error(
          `Expected ${okStatusCodes} status code, but got ${status} instead`,
        )
      }
    })
  },
)

/**
 * login()
 */
Cypress.Commands.add('login', () => {
  cy.fetch({
    url: '/api/v1/login/',
    method: 'POST',
    body: {
      username: Cypress.env('USERNAME'),
      password: Cypress.env('PASSWORD'),
    },
    log: false,
  })
})

/**
 * logout()
 */
Cypress.Commands.add('logout', () => {
  cy.fetch({ url: '/api/v1/logout/', method: 'POST', log: false })
})

/**
 * this.PROJECT_ID
 */
before(() => {
  cy.login()

  cy.fetch({ url: '/api/v1/projects/', log: false })
    .then(({ body: { results } }) => {
      const { id } = results.find(
        ({ name }) => name.toLowerCase() === 'cypress',
      )

      return id
    })
    .as('PROJECT_ID')

  cy.logout()
})
