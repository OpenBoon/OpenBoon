import TestRenderer, { act } from 'react-test-renderer'

import Timeline from '..'

const noop = () => {}

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'
const ASSET_ID = 'vZgbkqPftuRJ_-Of7mHWDNnJjUpFQs0C'

jest.mock('../Controls', () => 'TimelineControls')
jest.mock('../Captions', () => 'TimelineCaptions')
jest.mock('../Playhead', () => 'TimelinePlayhead')
jest.mock('../FilterTracks', () => 'TimelineFilterTracks')
jest.mock('../Ruler', () => 'TimelineRuler')
jest.mock('../Aggregate', () => 'TimelineAggregate')
jest.mock('../Timelines', () => 'TimelineTimelines')

describe('<Timeline />', () => {
  it('should render properly', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    const component = TestRenderer.create(
      <Timeline length={18} videoRef={{ current: undefined }} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should open the Timeline panel', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    Object.defineProperties(
      document,
      {
        getElementById: {
          value: () => ({ addEventListener: noop, removeEventListener: noop }),
          configurable: true,
        },
      },
      {},
    )

    const component = TestRenderer.create(
      <Timeline
        length={18}
        videoRef={{
          current: {
            play: noop,
            pause: noop,
            addEventListener: noop,
            removeEventListener: noop,
            currentTime: 0,
            duration: 18,
            paused: true,
          },
        }}
      />,
    )

    // Open timeline
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Open Timeline' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Close timeline
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Close Timeline' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    component.unmount()
  })

  it('should do nothing when the scroll container has not yet mounted', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    Object.defineProperties(
      document,
      {
        getElementById: {
          value: () => {},
          configurable: true,
        },
      },
      {},
    )

    const component = TestRenderer.create(
      <Timeline
        length={18}
        videoRef={{
          current: {
            play: noop,
            pause: noop,
            addEventListener: noop,
            removeEventListener: noop,
            currentTime: 0,
            duration: 18,
            paused: true,
          },
        }}
      />,
    )

    // Open timeline
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Open Timeline' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
