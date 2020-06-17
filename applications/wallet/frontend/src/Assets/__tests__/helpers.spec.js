import { formatSeconds } from '../helpers'

describe('<Assets /> helpers', () => {
  describe('formatSeconds()', () => {
    it('should format double digit hours', () => {
      expect(formatSeconds({ seconds: 40123 })).toEqual('11:08:43')
    })

    it('should format single digit hours', () => {
      expect(formatSeconds({ seconds: 30000 })).toEqual('8:20:00')
    })

    it('should format double digit minutes', () => {
      expect(formatSeconds({ seconds: 700 })).toEqual('11:40')
    })

    it('should format single digit minutes', () => {
      expect(formatSeconds({ seconds: 300 })).toEqual('5:00')
    })

    it('should format seconds', () => {
      expect(formatSeconds({ seconds: 40 })).toEqual('0:40')
    })
  })
})
