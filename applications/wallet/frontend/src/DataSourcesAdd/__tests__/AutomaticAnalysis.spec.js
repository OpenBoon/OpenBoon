import TestRenderer from 'react-test-renderer'

import DataSourcesAddAutomaticAnalysis from '../AutomaticAnalysis'

describe('<DataSourcesAddAutomaticAnalysis />', () => {
  it('should render properly with no fileTypes', async () => {
    const component = TestRenderer.create(
      <DataSourcesAddAutomaticAnalysis fileTypes={[]} />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
