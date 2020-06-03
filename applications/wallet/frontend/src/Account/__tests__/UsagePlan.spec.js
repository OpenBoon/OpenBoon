import TestRenderer from 'react-test-renderer'

import subscriptions from '../../Subscriptions/__mocks__/subscriptions'

import AccountUsagePlan from '../UsagePlan'

describe('<UsagePlan />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <AccountUsagePlan subscriptions={subscriptions.results} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with over usage', () => {
    const component = TestRenderer.create(
      <AccountUsagePlan
        subscriptions={[
          {
            ...subscriptions.results[0],
            usage: {
              videoHours: 320,
              imageCount: 30040,
            },
          },
        ]}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })

  it('should render properly with no modules', () => {
    const component = TestRenderer.create(
      <AccountUsagePlan
        subscriptions={[
          {
            ...subscriptions.results[0],
            modules: [],
          },
        ]}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
