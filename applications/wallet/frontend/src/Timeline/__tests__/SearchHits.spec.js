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
          width: 200,
          filter: '',
          timelines: { [timelines[0].timeline]: { isVisible: true } },
          zoom: 100,
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
