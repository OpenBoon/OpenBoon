import { getCurrentPeriod } from '../helpers'

describe('<OrganizationProjects /> helpers', () => {
  describe('getCurrentPeriod()', () => {
    it('should return formatted date string', () => {
      expect(getCurrentPeriod({ date: new Date(2021, 2, 13) })).toEqual(
        'March 1–13, 2021',
      )
    })
  })
})
