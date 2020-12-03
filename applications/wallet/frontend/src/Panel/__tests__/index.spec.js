import TestRenderer from 'react-test-renderer'

import Panel from '..'

import { constants } from '../../Styles'
import DashboardSvg from '../../Icons/dashboard.svg'

jest.mock('../../Resizeable', () => 'Resizeable')

describe('<Panel />', () => {
  it('should render properly with a selection', () => {
    localStorage.setItem(
      'rightOpeningPanelSettings',
      JSON.stringify({
        size: 400,
        originSize: 400,
        isOpen: true,
        openPanel: 'filters',
      }),
    )

    const component = TestRenderer.create(
      <Panel openToThe="right">
        {{
          filters: {
            title: 'Filters',
            icon: <DashboardSvg height={constants.icons.regular} />,
            content: <div />,
          },
        }}
      </Panel>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly without a selection', () => {
    const component = TestRenderer.create(
      <Panel openToThe="left">
        {{
          filters: {
            title: 'Filters',
            icon: <DashboardSvg height={constants.icons.regular} />,
            content: <div />,
          },
        }}
      </Panel>,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
