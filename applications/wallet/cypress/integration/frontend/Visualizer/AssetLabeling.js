describe('Visualizer', function () {
  describe('Asset Labels', function () {
    beforeEach(() => {
      /**
       * zooming in displays fewer assets
       * which causes less stress on the network
       * and triggers fewer errors
       */
      localStorage.setItem(
        'Assets',
        JSON.stringify({ columnCount: 1, isMin: false, isMax: true }),
      )
    })

    it.skip('can be created, updated, and deleted', function () {
      const now = Date.now()

      cy.login()

      /**
       * Create
       */
      cy.visit(`/${this.PROJECT_ID}/visualizer`)

      cy.get('button[aria-label="Add Labels To Model"]').click()

      cy.contains('Select an asset to add labels.')

      cy.selectFirstAsset()

      cy.url().should('match', /(.*)\/visualizer\?assetId=(.*)/)

      cy.get('summary[aria-label*="Asset Labels"]').click()

      cy.get('label')
        .contains('Model')
        .children()
        .children()
        .select('custom-model')

      cy.get('label').contains('Scope').children().children().select('Test')

      cy.get('label')
        .contains('Label')
        .children()
        .type(`Cypress-${now}`)
        .type('{enter}')

      /**
       * Update
       */
      cy.get('td').contains(`Cypress-${now}`).next().next().click()

      cy.contains('Edit Label').click()

      cy.contains('Label')
        .get('input')
        .type(`Cypress-${now}-again`)
        .type('{enter}')

      /**
       * Delete
       */
      cy.get('td').contains(`Cypress-${now}-again`).next().next().click()

      cy.contains('Delete Label').click()

      cy.contains('Delete Permanently').click()

      cy.contains(`Cypress-${now}-again`).should('not.exist')

      /*
       * Keyboard shortcut
       */
      cy.contains('Save Label').type('{rightarrow}')

      cy.contains('Save Label').type('s')

      cy.get('td').contains(`Cypress-${now}-again`).next().next().click()

      cy.contains('Delete Label').click()

      cy.contains('Delete Permanently').click()

      cy.contains(`Cypress-${now}-again`).should('not.exist')
    })
  })
})
