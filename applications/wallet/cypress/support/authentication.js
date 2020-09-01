/**
 * required ENV variable check
 */
if (!Cypress.env('USERNAME') || !Cypress.env('PASSWORD')) {
  throw new Error('Please provide a username and password.')
}

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
