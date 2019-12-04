import { getPageDescription } from '../helpers'

describe('getPageDescription', () => {
  describe('When the current page is not the last page', () => {
    it('Returns default string', () => {
      const resultString = 'Jobs: 1-5 of 6'

      expect(
        getPageDescription({
          pageSize: 5,
          pageIndex: 0,
          numRowsOnPage: 5,
          numRowsTotal: 6,
          canNextPage: true,
        }),
      ).toBe(resultString)
    })
  })
  describe('When the current page is the last page', () => {
    describe('Where is there one row', () => {
      it('Returns default string', () => {
        const resultString = 'Jobs: 6 of 6'

        expect(
          getPageDescription({
            pageSize: 5,
            pageIndex: 1,
            numRowsOnPage: 1,
            numRowsTotal: 6,
            canNextPage: false,
          }),
        ).toBe(resultString)
      })
    })
    describe('When there is more than one row', () => {
      it('Returns default string', () => {
        const resultString = 'Jobs: 6-7 of 7'

        expect(
          getPageDescription({
            pageSize: 5,
            pageIndex: 1,
            numRowsOnPage: 2,
            numRowsTotal: 7,
            canNextPage: false,
          }),
        ).toBe(resultString)
      })
    })
  })
})
