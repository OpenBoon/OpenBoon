import TestRenderer from 'react-test-renderer'

import ProgressBarLegend from '../Legend'

describe('<ProgressBarLegend />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <ProgressBarLegend
        state="Active"
        tasksProgress={{
          Failed: 1,
          Skipped: 0,
          Succeeded: 0,
          Running: 0,
          Pending: 0,
        }}
        timeStarted={1564699941318}
        timeUpdated={1565211006581}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
