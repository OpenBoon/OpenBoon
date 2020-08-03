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
  const { csrftoken } = Object.fromEntries(
    document.cookie.split(/; */).map((c) => {
      const [key, ...v] = c.split('=')
      return [key, decodeURIComponent(v.join('='))]
    }),
  )

  cy.request({
    method: 'POST',
    url: '/api/v1/logout/',
    headers: {
      'Content-Type': 'application/json;charset=UTF-8',
      'X-CSRFToken': csrftoken,
      Referer: Cypress.config('baseUrl'),
    },
  })
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
