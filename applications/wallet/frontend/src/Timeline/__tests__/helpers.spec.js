import {
  getIgnore,
  setIgnore,
  formatPaddedSeconds,
  updatePlayheadPosition,
  getRulerLayout,
  gotoPreviousHit,
  gotoNextHit,
  getScrollbarScrollableWidth,
} from '../helpers'

const noop = () => () => {}

describe('<Timeline /> helpers', () => {
  describe('get/setIgnore()', () => {
    expect(getIgnore()).toBe(false)

    setIgnore({ value: true })

    expect(getIgnore()).toBe(true)
  })

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
          offset: 0,
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

      updatePlayheadPosition({ video, playhead, zoom: 100, scrollLeft: 0 })

      expect(mockSetProperty).toHaveBeenCalledWith(
        'left',
        'calc(50% - 1px - 0px)',
      )
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

  describe('gotoPreviousHit()', () => {
    it('should round and sort', () => {
      const videoRef = {
        current: { pause: noop, currentTime: 4.9999, duration: 10 },
      }

      gotoPreviousHit({
        videoRef,
        timelines: [
          {
            tracks: [
              {
                track: 'gcp-logo-detection',
                hits: [{ start: 5.0 }, { start: 5.001 }, { start: 4.999 }],
              },
            ],
          },
        ],
        settings: { timelines: {}, filter: '' },
      })()

      expect(videoRef.current.currentTime).toBe(4.999)
    })

    it('should go to the previous hit', () => {
      const videoRef = {
        current: { pause: noop, currentTime: 5, duration: 10 },
      }

      gotoPreviousHit({
        videoRef,
        timelines: [
          { tracks: [{ track: 'gcp-logo-detection', hits: [{ start: 2 }] }] },
        ],
        settings: { timelines: {}, filter: '' },
      })()

      expect(videoRef.current.currentTime).toBe(2)
    })

    it('should go to the start of the clip', () => {
      const videoRef = {
        current: { pause: noop, currentTime: 5, duration: 10 },
      }

      gotoPreviousHit({
        videoRef,
        timelines: [
          { tracks: [{ track: 'gcp-logo-detection', hits: [{ start: 8 }] }] },
        ],
        settings: { timelines: {}, filter: '' },
      })()

      expect(videoRef.current.currentTime).toBe(0)
    })
  })

  describe('gotoNextHit()', () => {
    it('should go to the next hit', () => {
      const videoRef = {
        current: { pause: noop, currentTime: 5, duration: 10 },
      }

      gotoNextHit({
        videoRef,
        timelines: [
          { tracks: [{ track: 'gcp-logo-detection', hits: [{ start: 8 }] }] },
        ],
        settings: { timelines: {}, filter: '' },
      })()

      expect(videoRef.current.currentTime).toBe(8)
    })

    it('should go to the end of the clip', () => {
      const videoRef = {
        current: { pause: noop, currentTime: 5, duration: 10 },
      }

      gotoNextHit({
        videoRef,
        timelines: [
          { tracks: [{ track: 'gcp-logo-detection', hits: [{ start: 2 }] }] },
        ],
        settings: { timelines: {}, filter: '' },
      })()

      expect(videoRef.current.currentTime).toBe(10)
    })
  })

  describe('getScrollbarScrollableWidth', () => {
    it('should properly calculate the scrollable width', () => {
      expect(
        getScrollbarScrollableWidth({
          scrollbarRef: {},
          scrollbarTrackRef: {},
        }),
      ).toBe(0)
    })
  })
})
