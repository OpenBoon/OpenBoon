import { formatPaddedSeconds } from '../helpers'

describe('<Timeline /> helpers', () => {
  describe('formatPaddedSeconds()', () => {
    it('should format not a number', () => {
      expect(formatPaddedSeconds({ seconds: NaN })).toEqual('00:00:00')
    })

    it('should format double digit hours', () => {
      expect(formatPaddedSeconds({ seconds: 40123 })).toEqual('11:08:43')
    })

    it('should format single digit hours', () => {
      expect(formatPaddedSeconds({ seconds: 30000 })).toEqual('08:20:00')
    })

    it('should format double digit minutes', () => {
      expect(formatPaddedSeconds({ seconds: 700 })).toEqual('00:11:40')
    })

    it('should format single digit minutes', () => {
      expect(formatPaddedSeconds({ seconds: 300 })).toEqual('00:05:00')
    })

    it('should format seconds', () => {
      expect(formatPaddedSeconds({ seconds: 40 })).toEqual('00:00:40')
    })
  })
})
