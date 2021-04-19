describe('ProjectUsers', function () {
  const USER_EMAIL = 'not-cypress@zorroa.com'

  it('can be added', function () {
    cy.login()

    cy.visit(`/${this.PROJECT_ID}/users/add`)

    cy.get('input[name=emails]').type(USER_EMAIL)

    cy.contains('ML Tools').click()

    cy.get('button[type=submit]').contains('Add').click()

    cy.contains('Users added.')

    cy.contains(USER_EMAIL)

    cy.contains('ML Tools')
  })

  it('can be removed', function () {
    cy.login()

    cy.fetch({
      method: 'POST',
      url: `/api/v1/projects/${this.PROJECT_ID}/users/`,
      body: {
        batch: [{ email: USER_EMAIL, roles: ['ML_TOOLS'] }],
      },
      log: false,
    })

    cy.visit(`/${this.PROJECT_ID}/users`)

    cy.contains(USER_EMAIL).siblings().last().click()

    cy.get('button').contains('Remove').click()

    cy.get('button[type=button]').contains('Remove User').click()

    cy.contains(USER_EMAIL).should('not.exist')
  })
})
