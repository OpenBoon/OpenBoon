import {
  formatPaddedSeconds,
  updatePlayheadPosition,
  getRulerLayout,
  setScroll,
} from '../helpers'

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

  describe('updatePlayheadPosition()', () => {
    it('should do nothing when video or playhead are undefined', () => {
      expect(
        updatePlayheadPosition({
          video: undefined,
          playhead: undefined,
          zoom: 100,
        }),
      ).toBe(null)
    })

    it('should update the left position', () => {
      const video = {
        duration: 10,
        currentTime: 5,
      }

      const mockSetProperty = jest.fn()

      const playhead = {
        style: {
          setProperty: mockSetProperty,
        },
      }

      updatePlayheadPosition({ video, playhead, zoom: 100 })

      expect(mockSetProperty).toHaveBeenCalledWith('left', 'calc(50% - 1px)')
    })
  })

  describe('getRulerLayout()', () => {
    it('should render properly when all half seconds can be marked', () => {
      const { halfSeconds, majorStep } = getRulerLayout({
        length: 25.045,
        width: 1000,
      })
      expect(halfSeconds.length).toBe(50)
      expect(majorStep).toBe(4)
    })

    it('should render properly when marks are scaled to fit', () => {
      const { halfSeconds, majorStep } = getRulerLayout({
        length: 25.045,
        width: 775,
      })
      expect(halfSeconds.length).toBe(50)
      expect(majorStep).toBe(8)
    })
  })

  describe('setScroll()', () => {
    it('should scroll when there is more hidden content', () => {
      const { newScrollLeftPos, newScrollTopPos } = setScroll({
        event: { deltaX: 100, deltaY: 100 },
        scrollLeftPos: 0,
        scrollTopPos: 0,
        scrollablesX: [{ scrollWidth: 1000, clientWidth: 500 }],
        scrollablesY: [{ scrollHeight: 1000, clientHeight: 500 }],
      })
      expect(newScrollLeftPos).toBe(100)
      expect(newScrollTopPos).toBe(100)
    })

    it('should not scroll when there is no more hidden content', () => {
      const { newScrollLeftPos, newScrollTopPos } = setScroll({
        event: { deltaX: 100, deltaY: 100 },
        scrollLeftPos: 500,
        scrollTopPos: 500,
        scrollablesX: [{ scrollWidth: 1000, clientWidth: 500 }],
        scrollablesY: [{ scrollHeight: 1000, clientHeight: 500 }],
      })
      expect(newScrollLeftPos).toBe(500)
      expect(newScrollTopPos).toBe(500)
    })
  })
})
