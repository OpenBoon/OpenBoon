import TestRenderer from 'react-test-renderer'

import FilterExists from '..'

const PROJECT_ID = '76917058-b147-4556-987a-0a0f11e46d9b'

jest.mock('../../Filters/Reset', () => 'FiltersReset')

describe('<FilterExists />', () => {
  it('should render properly', () => {
    const component = TestRenderer.create(
      <FilterExists
        projectId={PROJECT_ID}
        assetId=""
        filters={[
          {
            type: 'exists',
            attribute: 'location.point',
            values: { exists: false },
          },
        ]}
        filter={{
          type: 'exists',
          attribute: 'location.point',
          values: { exists: false },
        }}
        filterIndex={0}
      />,
    )

    expect(component.toJSON()).toMatchSnapshot()
  })
})
