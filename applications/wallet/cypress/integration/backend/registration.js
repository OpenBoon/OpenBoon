describe('Authentication', function () {
  it('can log in and log out', function () {
    cy.login()

    cy.fetch({ url: '/api/v1/me/' })

    cy.fetch({ url: '/api/v1/logout/', method: 'POST' })

    cy.fetch({
      url: '/api/v1/me/',
      okStatusCodes: [403],
    })
  })
})
