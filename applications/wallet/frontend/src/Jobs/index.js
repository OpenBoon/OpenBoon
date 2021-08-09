import Head from 'next/head'
import { useRouter } from 'next/router'

import { colors, spacing, constants } from '../Styles'

import PageTitle from '../PageTitle'
import Table, { ROLES } from '../Table'
import Menu from '../Menu'
import MenuButton from '../Menu/Button'
import Checkbox, { VARIANTS as CHECKBOX_VARIANTS } from '../Checkbox'

import { encode, decode } from '../Filters/helpers'

import FilterSvg from '../Icons/filter.svg'

import JobsRow from './Row'

const STATES = ['InProgress', 'Cancelled', 'Success', 'Archived', 'Failure']

const Jobs = () => {
  const {
    query: { projectId, filters: query },
  } = useRouter()

  const filters = decode({ query })
  const states = filters?.states || []

  return (
    <>
      <Head>
        <title>Job Queue</title>
      </Head>

      <PageTitle>Job Queue</PageTitle>

      <Table
        role={ROLES.ML_Tools}
        legend="Jobs"
        url={`/api/v1/projects/${projectId}/jobs/`}
        refreshKeys={[]}
        refreshButton
        columns={[
          { key: 'state', label: 'Status' },
          { key: 'name', label: 'Job Name' },
          { key: 'priority', label: 'Priority' },
          { key: 'timeCreated', label: 'Created' },
          '# Assets',
          'Errors',
          'Task Progress',
          '#Actions#',
        ]}
        expandColumn={2}
        renderEmpty={
          <>
            <div>There are currently no jobs in the queue.</div>
            <div>Any new job will appear here.</div>
          </>
        }
        renderRow={({ result, revalidate }) => (
          <JobsRow
            key={result.id}
            projectId={projectId}
            job={result}
            revalidate={revalidate}
          />
        )}
        searchLabel="Job Name"
        options={{ refreshInterval: 5000 }}
        filters={({ onChange }) => {
          return (
            <div css={{ '> div > div': { top: 'auto' } }}>
              <Menu
                open="bottom-center"
                button={({ onBlur, onClick, isMenuOpen }) => (
                  <MenuButton
                    onBlur={onBlur}
                    onClick={onClick}
                    legend={
                      <div css={{ display: 'flex', alignItems: 'center' }}>
                        <FilterSvg
                          height={constants.icons.regular}
                          css={{
                            paddingRight: spacing.base,
                            marginTop: -spacing.small,
                          }}
                        />
                        Filter Status
                      </div>
                    }
                    style={{
                      height: '100%',
                      '&,&:hover,&:visited': {
                        backgroundColor: isMenuOpen
                          ? colors.structure.steel
                          : colors.structure.smoke,
                      },
                      '&:hover': {
                        backgroundColor: colors.structure.steel,
                      },
                    }}
                  />
                )}
              >
                {() => (
                  <div
                    // eslint-disable-next-line
                    tabIndex="1"
                    css={{
                      backgroundColor: colors.structure.iron,
                      overflowY: 'auto',
                      paddingTop: spacing.base,
                      paddingBottom: spacing.base,
                      label: {
                        paddingTop: spacing.base,
                        paddingBottom: spacing.base,
                      },
                    }}
                  >
                    {STATES.map((name) => (
                      <Checkbox
                        key={name}
                        variant={CHECKBOX_VARIANTS.SECONDARY}
                        option={{
                          value: name,
                          label: name.replace(
                            /([A-Z])/g,
                            (match) => ` ${match}`,
                          ),
                          initialValue: states.indexOf(name) > -1,
                          isDisabled: false,
                        }}
                        onClick={(value) => {
                          if (value) {
                            return onChange({
                              value: encode({
                                filters: {
                                  states: [...states, name],
                                },
                              }),
                            })
                          }

                          const index = states.indexOf(name)

                          const newFilters = {
                            states: [
                              ...states.slice(0, index),
                              ...states.slice(index + 1),
                            ],
                          }

                          return onChange({
                            value:
                              newFilters.states?.length > 0
                                ? encode({ filters: newFilters })
                                : undefined,
                          })
                        }}
                      />
                    ))}
                  </div>
                )}
              </Menu>
            </div>
          )
        }}
      />
    </>
  )
}

export default Jobs
