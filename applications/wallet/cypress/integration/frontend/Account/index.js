describe('Account', function () {
  it('can be created in Dev', function () {
    const now = Date.now()
    const firstName = `Cy-${now}`
    const lastName = `Press-${now}`
    const email = `cypress+${now}@zorroa.com`
    const password = btoa(now)

    cy.visit('/create-account')

    cy.get('input[name=firstName]').type(firstName)

    cy.get('input[name=lastName]').type(lastName)

    cy.get('input[name=email]').type(email)

    cy.get('input[name=password]').type(password)

    cy.get('input[name=confirmPassword]').type(password)

    cy.contains('Accept').click()

    cy.contains('Save').click()

    cy.contains('Confirmation email sent!')
  })

  it('can be created in QA', function () {
    const now = Date.now()
    const firstName = `Cy-${now}`
    const lastName = `Press-${now}`
    const email = `cypress+${now}@zorroa.com`
    const password = btoa(now)

    cy.visit('https://qa.boonai.app/create-account')

    cy.get('input[name=firstName]').type(firstName)

    cy.get('input[name=lastName]').type(lastName)

    cy.get('input[name=email]').type(email)

    cy.get('input[name=password]').type(password)

    cy.get('input[name=confirmPassword]').type(password)

    cy.contains('Accept').click()

    cy.contains('Save').click()

    cy.contains('Confirmation email sent!')
  })

  it('can be created in Prod', function () {
    const now = Date.now()
    const firstName = `Cy-${now}`
    const lastName = `Press-${now}`
    const email = `cypress+${now}@zorroa.com`
    const password = btoa(now)

    cy.visit('https://boonai.app/create-account')

    cy.get('input[name=firstName]').type(firstName)

    cy.get('input[name=lastName]').type(lastName)

    cy.get('input[name=email]').type(email)

    cy.get('input[name=password]').type(password)

    cy.get('input[name=confirmPassword]').type(password)

    cy.contains('Accept').click()

    cy.contains('Save').click()

    cy.contains('Confirmation email sent!')
  })

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
