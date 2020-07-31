describe('Users', function () {
  it('can login with proper credentials', function () {
    cy.visit('/')

    cy.get('input[name=username]').type(Cypress.env('USERNAME'))

    cy.get('input[name=password]').type(Cypress.env('PASSWORD')).type('{enter}')

    cy.contains('Account Overview')
  })

  it('can logout', function () {
    cy.login()

    cy.visit('/')

    cy.get('button[aria-label="Open user menu"]').click()

    cy.contains('Sign Out').click()

    cy.contains('Welcome. Please login.')
  })
})
