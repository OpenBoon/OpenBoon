import PropTypes from 'prop-types'
import { colors, constants, spacing, typography } from '../Styles'
import { formatFullDate } from '../Date/helpers'
import Status from '../Status'
import ProgressBar from '../ProgressBar'

const Table = ({ columns, rows }) => {
  return (
    <table
      css={{
        width: '100%',
        borderSpacing: 0,
        boxShadow: constants.boxShadows.dark,
        th: {
          fontWeight: typography.weight.medium,
          color: colors.structureShades.pebble,
          backgroundColor: colors.structureShades.iron,
          padding: `${spacing.moderate}px ${spacing.normal}px`,
          borderBottom: constants.borders.default,
          ':nth-of-type(2)': {
            width: '100%',
          },
          '&:not(:last-child)': {
            borderRight: constants.borders.default,
          },
        },
        tr: {
          ':hover': {
            backgroundColor: colors.structureShades.iron,
            boxShadow: constants.boxShadows.tableRow,
            td: {
              border: constants.borders.tableRow,
              borderLeft: '0',
              borderRight: '0',
              '&:first-of-type': {
                border: constants.borders.tableRow,
                borderRight: '0',
              },
              '&:last-of-type': {
                border: constants.borders.tableRow,
                borderLeft: '0',
              },
            },
          },
        },
        td: {
          whiteSpace: 'nowrap',
          fontWeight: typography.weight.extraLight,
          color: colors.structureShades.pebble,
          padding: `${spacing.base}px ${spacing.normal}px`,
          border: constants.borders.transparent,
          borderLeft: '0',
          borderRight: '0',
          ':first-of-type': {
            border: constants.borders.transparent,
            borderRight: '0',
          },
          ':last-of-type': {
            border: constants.borders.transparent,
            borderLeft: '0',
          },
        },
      }}>
      <thead>
        <tr>
          {columns.map(column => {
            return (
              <th key={column}>
                <div css={{ display: 'flex' }}>{column}</div>
              </th>
            )
          })}
        </tr>
      </thead>
      <tbody>
        {rows.map(row => {
          return (
            <tr
              key={row.id}
              css={{
                backgroundColor: colors.structureShades.lead,
                '&:nth-of-type(2n)': {
                  backgroundColor: colors.structureShades.mattGrey,
                },
              }}>
              <td>
                <Status jobStatus={row.status} />
              </td>
              <td>{row.jobName}</td>
              <td>{row.createdBy}</td>
              <td>{row.priority}</td>
              <td>{formatFullDate({ timestamp: row.createdDateTime })}</td>
              <td>
                {row.failed > 0 && (
                  <div
                    css={{
                      display: 'flex',
                      justifyContent: 'center',
                      color: colors.failed,
                      fontWeight: typography.weight.bold,
                      fontSize: typography.size.kilo,
                      lineHeight: `${typography.size.mega}px`,
                      padding: spacing.base,
                      borderRadius: 32,
                      backgroundColor: colors.structureShades.coal,
                    }}>
                    {row.failed}
                  </div>
                )}
              </td>
              <td>
                {row.errors > 0 && (
                  <div
                    css={{
                      display: 'flex',
                      justifyContent: 'center',
                      color: colors.failed,
                      fontWeight: typography.weight.bold,
                      fontSize: typography.kilo,
                      lineHeight: typography.mega,
                      padding: spacing.base,
                      borderRadius: 32,
                      backgroundColor: colors.structureShades.coal,
                    }}>
                    {row.errors}
                  </div>
                )}
              </td>
              <td>{row.numAssets}</td>
              <td>
                <ProgressBar status={row.progress} />
              </td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}

Table.propTypes = {
  columns: PropTypes.arrayOf(PropTypes.string).isRequired,
  rows: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string,
      status: PropTypes.string,
      jobName: PropTypes.string,
      createdBy: PropTypes.string,
      priority: PropTypes.number,
      createdDateTime: PropTypes.number,
      failed: PropTypes.node,
      errors: PropTypes.node,
      numAssets: PropTypes.string,
      progress: PropTypes.shape({
        isGenerating: PropTypes.bool,
        isCanceled: PropTypes.bool,
        canceledBy: PropTypes.string,
        succeeded: PropTypes.number,
        failed: PropTypes.number,
        running: PropTypes.number,
        pending: PropTypes.number,
      }),
    }),
  ).isRequired,
}

export default Table
