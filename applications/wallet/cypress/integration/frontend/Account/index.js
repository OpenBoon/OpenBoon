describe('Account', function () {
  it('can update first and last name', function () {
    const now = Date.now()
    const firstName = `Cy-${now}`
    const lastName = `Press-${now}`

    cy.login()

    cy.visit('/account')

    cy.contains('Edit').click()

    cy.get('input[name=firstName]').clear().type(firstName)

    cy.get('input[name=lastName]').clear().type(lastName).type('{enter}')

    cy.contains('New name saved.')

    cy.contains(firstName)

    cy.contains(lastName)
  })
})
