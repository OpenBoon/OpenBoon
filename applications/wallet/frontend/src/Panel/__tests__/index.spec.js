import TestRenderer, { act } from 'react-test-renderer'

import Panel from '..'

import { constants } from '../../Styles'
import DashboardSvg from '../../Icons/dashboard.svg'

jest.mock('../../Resizeable', () => 'Resizeable')

describe('<Panel />', () => {
  it('should render properly opening to the right', () => {
    const component = TestRenderer.create(
      <Panel openToThe="right">
        {{
          filters: {
            title: 'Filters',
            icon: <DashboardSvg height={constants.icons.regular} />,
            content: '',
          },
        }}
      </Panel>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Open Panel with Icon
    act(() => {
      component.root.findByProps({ 'aria-label': 'Filters' }).props.onClick()
    })

    // Close Panel with Icon
    act(() => {
      component.root.findByProps({ 'aria-label': 'Filters' }).props.onClick()
    })

    // Open Panel with Icon
    act(() => {
      component.root.findByProps({ 'aria-label': 'Filters' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()

    // Resize large
    act(() => {
      component.root.findByType('Resizeable').props.onMouseUp({ size: 500 })
    })

    // Resize to close
    act(() => {
      component.root.findByType('Resizeable').props.onMouseUp({ size: 100 })
    })

    // Open Panel with Icon
    act(() => {
      component.root.findByProps({ 'aria-label': 'Filters' }).props.onClick()
    })

    // Close Panel with Chevron
    act(() => {
      component.root
        .findByProps({ 'aria-label': 'Close Panel' })
        .props.onClick()
    })
  })

  it('should render properly opening to the left', () => {
    const component = TestRenderer.create(
      <Panel openToThe="left">
        {{
          metadata: {
            title: 'Metadata',
            icon: <DashboardSvg height={constants.icons.regular} />,
            content: '',
            isBeta: true,
          },
        }}
      </Panel>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Open Panel with Icon
    act(() => {
      component.root.findByProps({ 'aria-label': 'Metadata' }).props.onClick()
    })

    expect(component.toJSON()).toMatchSnapshot()
  })
})
