import TestRenderer, { act } from 'react-test-renderer'

import timelines from '../__mocks__/timelines'

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
jest.mock('../SearchHits', () => 'TimelineSearchHits')
jest.mock('../Timelines', () => 'TimelineTimelines')
jest.mock('../Resize', () => 'TimelineResize')

describe('<Timeline />', () => {
  it('should render properly', () => {
    const query = btoa(
      JSON.stringify([
        {
          type: 'textContent',
          attribute: '',
          values: { query: 'Lemon' },
        },
      ]),
    )

    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID, query },
    })

    require('swr').__setMockUseSWRResponse({
      data: timelines,
    })

    const component = TestRenderer.create(
      <Timeline length={18} videoRef={{ current: undefined }} />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Filter Search Highlights Only
    act(() => {
      component.root.findByProps({ value: 'highlights' }).props.onClick()
    })

    // Scroll timeline with mousewheel
    act(() => {
      component.root.findByProps({ 'aria-label': 'Timeline' }).props.onWheel()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should open the Timeline panel', () => {
    require('next/router').__setUseRouter({
      query: { projectId: PROJECT_ID, assetId: ASSET_ID },
    })

    require('swr').__setMockUseSWRResponse({
      data: [],
    })

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

    // CLose timeline
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Close Timeline' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Open timeline
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Open Timeline' })
        .props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
