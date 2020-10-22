import TestRenderer from 'react-test-renderer'

import timelines from '../__mocks__/timelines'

import TimelineSearchHits from '../SearchHits'

describe('<TimelineSearchHits />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <TimelineSearchHits
        videoRef={{ current: undefined }}
        length={16}
        timelineHeight={400}
        timelines={timelines}
        settings={{
          filter: '',
          highlights: false,
          width: 200,
          zoom: 100,
          timelines: { [timelines[0].timeline]: { isVisible: true } },
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
