import {PageContainer, ProFormSelect, ProFormText, QueryFilter} from '@ant-design/pro-components';
import {Avatar, Card, Flex, Image, Input, List, message, Tabs, Tag, Typography} from 'antd';
import React, {useEffect, useState} from 'react';
import {
  listFastGeneratorVoByPageUsingPost,
  listGeneratorVoByPageUsingPost
} from "@/services/backend/generatorController";
import moment from "moment";
import {UserOutlined} from "@ant-design/icons";
import {Link} from "umi";

/**
 * 默认分页参数
 */
const DEFAULT_PAGE_PARAMS: PageRequest = {
  current: 1,
  pageSize: 8,
  sortField: 'createTime',
  sortOrder: 'descend',
};

const IndexPage: React.FC = () => {

  /**
   * 搜索条件
   */
  const [searchParams, setSearchParams] = useState<API.GeneratorQueryRequest>({
    ...DEFAULT_PAGE_PARAMS,
  })

  /**
   * 是否加载
   */
  const [loading, setLoading] = useState<boolean>(true);

  /**
   * 数据列表
   */
  const [dataList, setDataList] = useState<API.GeneratorVO[]>([]);

  /**
   * 总页数
   */
  const [total, setTotal] = useState<number>(0);

  /**
   * 标签列表
   */
  const tagListView = (tags?: string[]) => {
    if (!tags) {
      return <></>;
    }
    return (
      <div style={{marginBottom: 8}}>
        {tags.map((tag) => (
          <Tag key={tag}>{tag}</Tag>
        ))}
      </div>
    );
  };

  /**
   * 搜索
   */
  const search = async () => {
    setLoading(true);
    try {
      const result = await listFastGeneratorVoByPageUsingPost(searchParams);
      setDataList(result.data?.records ?? []);
      setTotal(Number(result.data?.total) ?? 0);
    } catch (error: any) {
      message.error('获取数据失败，' + error.message);
    }
    setLoading(false);
  };

  /**
   * 监听 searchParams 变化
   */
  useEffect(() => {
    search();
  }, [searchParams]);

  return (
    <PageContainer title={<></>}>
      <Flex justify="center">
        <Input.Search
          style={{
            width: '40vw',
            minWidth: 320,
          }}
          placeholder="搜索代码生成器"
          allowClear
          enterButton="搜索"
          size="large"
          onChange={(e) => {
            searchParams.searchText = e.target.value;
          }}
          onSearch={(value: string) => {
            setSearchParams({
              ...searchParams,
              ...DEFAULT_PAGE_PARAMS,
              searchText: value,
            });
          }}
        />
      </Flex>
      <div style={{marginBottom: 16}}/>

      <Tabs
        size="large"
        defaultActiveKey="newest"
        items={[
          {
            key: 'newest',
            label: '最新',
          },
          {
            key: 'recommend',
            label: '推荐',
          },
        ]}
        onChange={() => {
        }}
      />

      <QueryFilter
        span={12}
        labelWidth="auto"
        labelAlign="left"
        defaultCollapsed={true}
        split
        style={{padding: '16px 0'}}
        onFinish={async (values: API.GeneratorQueryRequest) => {
          setSearchParams({
            ...DEFAULT_PAGE_PARAMS,
            // @ts-ignore
            ...values,
            searchText: searchParams.searchText,
          });
        }}
      >
        <ProFormSelect name="tags" label="标签" mode="tags"/>
        <ProFormText name="name" label="名称"/>
        <ProFormText name="description" label="描述"/>
      </QueryFilter>

      {/*卡片*/}
      <List<API.GeneratorVO>
        rowKey="id"
        loading={loading}
        grid={{
          gutter: 16,
          xs: 1,
          sm: 2,
          md: 3,
          lg: 3,
          xl: 4,
          xxl: 4,
        }}
        dataSource={dataList}
        pagination={{
          current: searchParams.current,
          pageSize: searchParams.pageSize,
          total,
          onChange: (current, pageSize) => {
            setSearchParams({
              ...searchParams,
              current,
              pageSize,
            });
          },
        }}
        renderItem={(data) => (
          <List.Item>
            <Link to={`/generator/detail/${data.id}`}>
              <Card hoverable cover={<Image alt={data.name} src={data.picture}/>}>
                <Card.Meta
                  title={<a>{data.name}</a>}
                  description={
                    <Typography.Paragraph
                      ellipsis={{
                        rows: 2,
                      }}
                    >
                      {data.description}
                    </Typography.Paragraph>
                  }
                />
                {tagListView(data.tags)}
                <Flex justify="space-between" align="center">
                  <Typography.Text type="secondary" style={{fontSize: 12}}>
                    {moment(data.createTime).fromNow()}
                  </Typography.Text>
                  <div>
                    <Avatar src={data.user?.userAvatar ?? <UserOutlined/>}/>
                  </div>
                </Flex>
              </Card>
            </Link>
          </List.Item>
        )}
      />


    </PageContainer>
  );
};

export default IndexPage;
