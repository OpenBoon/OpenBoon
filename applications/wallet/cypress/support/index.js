if (!Cypress.env('USERNAME') || !Cypress.env('PASSWORD')) {
  throw new Error('Please provide a username and password.')
}

Cypress.on('uncaught:exception', (err, runnable) => {
  return false
})

Cypress.Commands.add('login', () => {
  cy.request('POST', '/api/v1/login/', {
    username: Cypress.env('USERNAME'),
    password: Cypress.env('PASSWORD'),
  })
})

Cypress.Commands.add('logout', () => {
  cy.apiRequest({ method: 'POST', url: '/api/v1/logout/', body: {} })
})

before(() => {
  cy.login()

  cy.request('GET', '/api/v1/projects/')
    .then(({ body: { results } }) => {
      const { id } = results.find(
        ({ name }) => name.toLowerCase() === 'cypress',
      )

      return id
    })
    .as('PROJECT_ID')

  cy.logout()
})

function getCookies() {
  return Object.fromEntries(
    document.cookie.split(/; */).map((c) => {
      const [key, ...v] = c.split('=')
      return [key, decodeURIComponent(v.join('='))]
    }),
  )
}

/** Extends the cy.request function to have the headers needed to use the Wallet REST api.
 * For convenience it allows you to set an array of acceptable status codes for the
 * response as well. */
Cypress.Commands.add(
  'apiRequest',
  ({ method, url, body, okStatusCodes = [200, 201] }) => {
    cy.request({
      method,
      url,
      body,
      failOnStatusCode: false,
      headers: {
        'Content-Type': 'application/json;charset=UTF-8',
        'X-CSRFToken': getCookies().csrftoken,
        Referer: Cypress.config('baseUrl'),
      },
    }).then((response) => {
      expect(response.status).to.be.oneOf(okStatusCodes)
    })
  },
)
