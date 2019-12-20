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
          tasksPending: 0,
        }}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})

describe('setShowKeyInfo()', () => {
  describe('when user is using mouse', () => {
    it('should show Legend on mouseenter', () => {
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
            tasksPending: 0,
          }}
        />,
      )

      expect(component.toJSON()).toMatchSnapshot()

      act(() => {
        component.root
          .findByProps({ 'aria-label': 'Progress Bar' })
          .props.onMouseEnter({ preventDefault: noop })
      })

      expect(component.toJSON()).toMatchSnapshot()

      act(() => {
        component.root
          .findByProps({ 'aria-label': 'Progress Bar' })
          .props.onMouseLeave({ preventDefault: noop })
      })

      expect(component.toJSON()).toMatchSnapshot()
    })
  })

  describe('when user is using keyboard', () => {
    it('should show Legend on keyPress', () => {
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
            tasksPending: 0,
          }}
        />,
      )

      expect(component.toJSON()).toMatchSnapshot()

      act(() => {
        component.root
          .findByProps({ 'aria-label': 'Progress Bar' })
          .props.onKeyPress({ preventDefault: noop })
      })

      expect(component.toJSON()).toMatchSnapshot()

      act(() => {
        component.root
          .findByProps({ 'aria-label': 'Progress Bar' })
          .props.onKeyPress({ preventDefault: noop })
      })

      expect(component.toJSON()).toMatchSnapshot()
    })
  })
})
