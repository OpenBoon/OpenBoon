import PropTypes from 'prop-types'
import { Responsive, WidthProvider } from 'react-grid-layout'

import chartShape from '../Chart/shape'

import { spacing } from '../Styles'

import { useLocalStorageState } from '../LocalStorage/helpers'

import ChartFacet from '../ChartFacet'
import ChartRange from '../ChartRange'

import { MIN_ROW_HEIGHT, breakpoints, cols, setAllLayouts } from './helpers'

const ResponsiveGridLayout = WidthProvider(Responsive)

const Charts = ({ charts, dispatch }) => {
  const initialLayouts = {
    9: charts.map(({ id }) => ({
      i: id,
      x: 0,
      y: 0,
      w: 2,
      minW: 2,
      h: 4,
      minH: 4,
    })),
  }

  const [layouts, setLayouts] = useLocalStorageState({
    key: '__TODO__',
    initialValue: initialLayouts,
  })

  return (
    <ResponsiveGridLayout
      layouts={layouts}
      breakpoints={breakpoints}
      cols={cols}
      margin={[spacing.normal, spacing.normal]}
      rowHeight={MIN_ROW_HEIGHT}
      onLayoutChange={setAllLayouts({ setLayouts })}
    >
      {charts
        .filter(({ id }) => !!id)
        .map((chart, index) => {
          switch (chart.type) {
            case 'facet': {
              return (
                <div key={chart.id}>
                  <ChartFacet
                    chart={chart}
                    chartIndex={index}
                    dispatch={dispatch}
                  />
                </div>
              )
            }

            case 'range': {
              return (
                <div key={chart.id}>
                  <ChartRange
                    chart={chart}
                    chartIndex={index}
                    dispatch={dispatch}
                  />
                </div>
              )
            }

            default:
              return null
          }
        })}
    </ResponsiveGridLayout>
  )
}

Charts.propTypes = {
  charts: PropTypes.arrayOf(PropTypes.shape(chartShape).isRequired).isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default Charts
