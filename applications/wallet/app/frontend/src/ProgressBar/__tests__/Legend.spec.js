import TestRenderer from 'react-test-renderer'

import ProgressBarLegend from '../Legend'

describe('<ProgressBarLegend />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <ProgressBarLegend
        taskCounts={{
          tasksFailure: 1,
          tasksSkipped: 0,
          tasksSuccess: 0,
          tasksRunning: 0,
          tasksPending: 0,
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
