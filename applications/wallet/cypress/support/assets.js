/**
 * Select first Asset in the Visualizer
 */

Cypress.Commands.add('selectFirstAsset', () => {
  const log = Cypress.log({
    name: 'selectFirstAsset',
    displayName: 'select',
    message: 'first asset in the visualizer',
  })

  cy.get('a[aria-label*="Select asset"]', { log: false })
    .first({ log: false })
    // element tends to rerender/detach, which confuses cypress
    // so we pass `{ force: true }` to keep going
    // cf https://github.com/cypress-io/cypress/issues/7306
    .click({ force: true, log: false })
    .then(($a) => {
      log.set({ $a }).snapshot()
      cy.url({ log: false }).should('eq', $a[0].href)
    })

  cy.get('a[aria-label*="Select asset"]', { log: false })
    .first({ log: false })
    .find('img', { log: false })
    .should('be.visible')
})
