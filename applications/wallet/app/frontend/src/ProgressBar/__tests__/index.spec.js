import TestRenderer, { act } from 'react-test-renderer'

import ProgressBar from '..'

const noop = () => () => {}

describe('<ProgressBar />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <ProgressBar
        state="Active"
        timeStarted={1564699941318}
        timeUpdated={1565211006581}
        taskCounts={{
          tasksFailure: 1,
          tasksSkipped: 0,
          tasksSuccess: 0,
          tasksRunning: 0,
          tasksWaiting: 0,
          tasksQueued: 0,
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
describe('setShowKeyInfo()', () => {
  it('should change state.showKeyInfo', () => {
    const component = TestRenderer.create(
      <ProgressBar
        state="Active"
        timeStarted={1564699941318}
        timeUpdated={1565211006581}
        taskCounts={{
          tasksFailure: 1,
          tasksSkipped: 0,
          tasksSuccess: 0,
          tasksRunning: 0,
          tasksWaiting: 0,
          tasksQueued: 0,
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByType('div')
        .props.onMouseEnter({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()

    act(() => {
      component.root
        .findByType('div')
        .props.onMouseLeave({ preventDefault: noop })
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
