import TestRenderer, { act } from 'react-test-renderer'

import PanelHeader from '../Header'

import { constants } from '../../Styles'
import DashboardSvg from '../../Icons/dashboard.svg'

const MIN_WIDTH = 400

describe('<PanelHeader />', () => {
  it('should open properly', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <PanelHeader openPanel="" dispatch={mockFn} minWidth={MIN_WIDTH}>
        {{
          filters: {
            title: 'Filters',
            icon: <DashboardSvg height={constants.icons.regular} />,
            content: <div />,
          },
          metadata: {
            title: 'Metadata',
            icon: <DashboardSvg height={constants.icons.regular} />,
            content: <div />,
            isBeta: true,
          },
        }}
      </PanelHeader>,
    )

    expect(component.toJSON()).toMatchSnapshot()

    // Open Panel
    act(() => {
      component.root.findByProps({ 'aria-label': 'Filters' }).props.onClick()
    })

    expect(mockFn).toHaveBeenCalledWith({
      type: 'OPEN',
      payload: { openPanel: 'filters', minSize: MIN_WIDTH },
    })
  })

  it('should close properly', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <PanelHeader openPanel="filters" dispatch={mockFn} minWidth={MIN_WIDTH}>
        {{
          filters: {
            title: 'Filters',
            icon: <DashboardSvg height={constants.icons.regular} />,
            content: <div />,
          },
          metadata: {
            title: 'Metadata',
            icon: <DashboardSvg height={constants.icons.regular} />,
            content: <div />,
            isBeta: true,
          },
        }}
      </PanelHeader>,
    )

    // Close Panel
    act(() => {
      component.root.findByProps({ 'aria-label': 'Filters' }).props.onClick()
    })

    expect(mockFn).toHaveBeenCalledWith({
      type: 'CLOSE',
      payload: { openPanel: '' },
    })
  })

  it('should swap sections properly', () => {
    const mockFn = jest.fn()

    const component = TestRenderer.create(
      <PanelHeader openPanel="filters" dispatch={mockFn} minWidth={MIN_WIDTH}>
        {{
          filters: {
            title: 'Filters',
            icon: <DashboardSvg height={constants.icons.regular} />,
            content: <div />,
          },
          metadata: {
            title: 'Metadata',
            icon: <DashboardSvg height={constants.icons.regular} />,
            content: <div />,
            isBeta: true,
          },
        }}
      </PanelHeader>,
    )

    // Close Panel
    act(() => {
      component.root.findByProps({ 'aria-label': 'Metadata' }).props.onClick()
    })

    expect(mockFn).toHaveBeenCalledWith({
      type: 'UPDATE',
      payload: { openPanel: 'metadata' },
    })
  })
})
